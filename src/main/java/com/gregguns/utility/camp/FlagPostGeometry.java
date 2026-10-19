package com.gregguns.utility.camp;

import java.util.Collections;
import java.util.List;

/**
 * Geometry of the Camp Flag post and its waving flag, as data. The renderer
 * ({@code com.gregguns.client.utility.CampFlagRenderer}) reads only this class.
 *
 * <p>
 * Format. All lengths are model units of 1/16 block. The origin is the bottom center of the block (x and z centered on
 * the block), +X east, +Y up, +Z south. A {@link Box} is its minimum corner plus its size, and a texture offset (u, v)
 * on a {@value #TEXTURE_SIZE} by {@value #TEXTURE_SIZE} unit sheet. {@link #POST_BOXES} are static. {@link #PANELS} are
 * flag segments chained from the pole toward the west (-X): panel i hangs at its {@code hingeX}, measured from the pole
 * center for the first panel and from the previous panel's hinge after that, and swings about its vertical edge by
 * {@code amplitudeDegrees * sin(2 pi (t mod period) / period)}, where t is the world time in ticks. Each panel's swing
 * is relative to the previous one, so the cloth ripples. Common side, no Minecraft types.
 */
// Spec: docs/gregguns-spec/09-machines-utility/index.md (Camp Flag); built from
// docs/techguns-spec/09-machines-utility/model-notes/flag_post.md (clean-room notes), not from any Techguns file
public final class FlagPostGeometry {

    /** Side length of the texture sheet in model units. */
    public static final int TEXTURE_SIZE = 64;
    /** Texture sheet resource path, relative to {@code assets/gregguns/}. */
    public static final String TEXTURE = "textures/models/machines/flagpost.png";
    /** Height of the whole post in model units; the renderer's bounding box is derived from it. */
    public static final int HEIGHT = 64;

    /** One axis-aligned box of the model. */
    public static final class Box {

        /** Minimum corner X. */
        public final float x;
        /** Minimum corner Y (up). */
        public final float y;
        /** Minimum corner Z. */
        public final float z;
        /** Size along X. */
        public final float sizeX;
        /** Size along Y. */
        public final float sizeY;
        /** Size along Z; 0 makes a flat plane. */
        public final float sizeZ;
        /** Texture U of the box's top-left unfolded corner. */
        public final int u;
        /** Texture V of the box's top-left unfolded corner. */
        public final int v;

        /** All fields in declaration order. */
        public Box(float x, float y, float z, float sizeX, float sizeY, float sizeZ, int u, int v) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.u = u;
            this.v = v;
        }
    }

    /** One swinging flag segment: a flat box and the numbers of its swing. */
    public static final class Panel {

        /** The cloth plane; its x is relative to the hinge. */
        public final Box box;
        /** Hinge X: from the pole center for the first panel, from the previous panel's hinge after that. */
        public final float hingeX;
        /** Swing amplitude in degrees. */
        public final float amplitudeDegrees;
        /** Swing period in world ticks. */
        public final int periodTicks;

        /** All fields in declaration order. */
        public Panel(Box box, float hingeX, float amplitudeDegrees, int periodTicks) {
            this.box = box;
            this.hingeX = hingeX;
            this.amplitudeDegrees = amplitudeDegrees;
            this.periodTicks = periodTicks;
        }
    }

    /**
     * The static post, stacked and centered on the pole axis (notes sections 2 and 3): base plate 10x2x10, plinth
     * 5x14x5, lower pole 3x23x3, upper pole 2x23x2 and cap 3x2x3, 64 units tall in all. Texture layout on the 64x64
     * sheet: the plate unwraps at (0,0), the cap at (40,0), the plinth at (0,12), the lower pole at (20,12) and the
     * upper pole at (32,12).
     */
    public static final List<Box> POST_BOXES = Collections.unmodifiableList(
        java.util.Arrays.asList(
            new Box(-5, 0, -5, 10, 2, 10, 0, 0),
            new Box(-2.5f, 2, -2.5f, 5, 14, 5, 0, 12),
            new Box(-1.5f, 16, -1.5f, 3, 23, 3, 20, 12),
            new Box(-1, 39, -1, 2, 23, 2, 32, 12),
            new Box(-1.5f, 62, -1.5f, 3, 2, 3, 40, 0)));

    /**
     * The three flag panels (notes section 3 and 7), each 6 wide, 14 tall and flat, hanging from the pole's west face
     * at Y 47.5 to 61.5 and reaching west (-X): the box spans -6..0 from its own hinge. The first hinge is 1 unit west
     * of the pole center (the upper pole's west face); the others sit 6 units further west. Each cloth plane has its
     * two sides at u (north side) and u + 6 (south side) on the sheet, row v = 50.
     */
    public static final List<Panel> PANELS = Collections.unmodifiableList(
        java.util.Arrays.asList(
            new Panel(new Box(-6, 47.5f, 0, 6, 14, 0, 0, 50), -1, 30f, 60),
            new Panel(new Box(-6, 47.5f, 0, 6, 14, 0, 12, 50), -6, 30f, 50),
            new Panel(new Box(-6, 47.5f, 0, 6, 14, 0, 24, 50), -6, 30f, 40)));

    private FlagPostGeometry() {}

    /**
     * @return the swing angle of one panel
     * @param panel     the panel
     * @param worldTime the world time in ticks (the model steps once per tick, with no partial-tick smoothing)
     */
    public static float swingDegrees(Panel panel, float worldTime) {
        float phase = (worldTime % panel.periodTicks) / panel.periodTicks;
        return (float) (panel.amplitudeDegrees * Math.sin(2 * Math.PI * phase));
    }

    /**
     * @return the farthest horizontal distance, in model units, any part of the model can reach from the pole axis:
     *         the flag's chained length plus the first hinge's offset, which the swing can turn in any direction
     */
    public static float horizontalReach() {
        float reach = 0;
        float chain = 0;
        for (Panel p : PANELS) {
            chain += Math.abs(p.hingeX);
            reach = Math.max(reach, chain + Math.abs(p.box.x));
        }
        return Math.max(reach, 5.0F);
    }
}
