package com.gregguns.client.render;

import com.gregguns.client.render.profile.TransformProgram;

/**
 * The item frame a held gun is drawn in, after the hand's own transform. Forge's {@code RenderPlayer} puts every item
 * whose renderer asks for the 3D render helper ({@link GunItemRenderer} does) in this frame; every gun's third-person
 * rows, and the fit in {@code InHandFit}, are written for it. Forge's {@code RenderBiped} does the same only for block
 * items, so a gun held by a mob would fall into vanilla's tool frame (scale 0.625, tilted back) and point the wrong
 * way; the NPC renderer applies this frame itself. Pure; the caller supplies the sink, so a unit test checks the ops.
 */
// Source: Minecraft 1.7.10 RenderPlayer.renderSpecials (func_77029_c), the isFull3D-independent "is3D" branch, as
// patched by Forge 10.13.4.1614.
// Decompiled: the sources jar the build produces (srg_patched_minecraft-sources) showed this branch.
public final class HeldGunFrame {

    /** Scale of the frame: 0.5 times 0.75, vanilla's block-in-hand scale. */
    public static final double SCALE = 0.375;

    private HeldGunFrame() {}

    /**
     * Applies the frame: translate (0, 0.1875, -0.3125), rotX 20, rotY 45, then scale (-s, -s, s).
     *
     * @param sink where the ops go
     */
    public static void apply(TransformProgram.OpSink sink) {
        sink.translate(0.0, 0.1875, -0.3125);
        sink.rotate(20.0, 1.0, 0.0, 0.0);
        sink.rotate(45.0, 0.0, 1.0, 0.0);
        sink.scale(-SCALE, -SCALE, SCALE);
    }
}
