package com.gregguns.client.mob;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gregguns.mob.model.MobBoxDef;
import com.gregguns.mob.model.MobModelDef;
import com.gregguns.mob.model.MobPartDef;

/**
 * Where a biped model's feet are, and the translation that puts them on the ground at any scale. Pure math over the
 * model definition, kept free of Minecraft classes so tests can run headless.
 *
 * <p>
 * The living renderer draws a model in a Y-down space: it flips Y, calls {@code preRenderCallback} (where the mob
 * renderers scale), then translates by -24 model units (and vanilla's 1/128-block lift), so model Y = 24 lands on the
 * entity's feet. Because the scale comes before that translation, it scales about the feet: a model whose feet are at
 * Y = 24 stands on the ground at every scale and needs no lift. A model whose lowest leg point is elsewhere is moved by
 * {@code (24 - footY) / 16} in the scaled space right after the scale, which lands its feet on the ground at every
 * scale too.
 */
// Spec: tools/asset-gen/entity_models.md; docs/techguns-spec/10-entities-npcs/rendering-and-models.md
final class MobFooting {

    /** Model Y, in model units, that the renderer puts on the entity's feet. */
    static final float FEET_Y = 24.0F;
    /** Model units per block. */
    private static final float UNITS_PER_BLOCK = 16.0F;

    private MobFooting() {}

    /**
     * The lowest point of a biped's legs at rest: the two leg parts ({@code rightLeg}, {@code leftLeg}) and every part
     * under them, each box with its inflate, after the rest rotations of the part chain. Other parts are left out, so
     * a coat or a held prop that hangs below the feet does not lift the mob.
     *
     * @param def a biped model definition
     * @return the largest Y (Y points down) of any leg box corner, in model units
     */
    static float restFootY(MobModelDef def) {
        return footY(def, 0.0F);
    }

    /**
     * The lowest point of a biped's legs with both legs pitched by the same extra angle about X, as the walk swing does
     * (one leg forward, one back by the same amount: the result is the same for either sign, since it takes the lower
     * of the two legs).
     *
     * @param def             a biped model definition
     * @param legPitchDegrees extra rotation about X added to each leg root's rest rotation, in degrees
     * @return the largest Y of any leg box corner, in model units
     */
    static float footY(MobModelDef def, float legPitchDegrees) {
        List<MobPartDef> parts = def.parts();
        Set<String> legs = new HashSet<>();
        legs.add("rightLeg");
        legs.add("leftLeg");
        // Parents may be listed after their children: grow the leg set until it stops changing.
        boolean grew = true;
        while (grew) {
            grew = false;
            for (MobPartDef p : parts) {
                if (p.parent() != null && legs.contains(p.parent()) && legs.add(p.name())) {
                    grew = true;
                }
            }
        }
        float lowest = Float.NEGATIVE_INFINITY;
        for (MobPartDef p : parts) {
            if (!legs.contains(p.name())) {
                continue;
            }
            for (MobBoxDef b : p.boxes()) {
                float[] o = b.offset();
                int[] s = b.size();
                float g = b.inflate();
                for (int corner = 0; corner < 8; corner++) {
                    float[] v = { (corner & 1) == 0 ? o[0] - g : o[0] + s[0] + g,
                        (corner & 2) == 0 ? o[1] - g : o[1] + s[1] + g,
                        (corner & 4) == 0 ? o[2] - g : o[2] + s[2] + g };
                    lowest = Math.max(lowest, toModel(def, p, v, legPitchDegrees)[1]);
                }
            }
        }
        if (lowest == Float.NEGATIVE_INFINITY) {
            throw new IllegalArgumentException("model '" + def.name() + "' has no leg boxes");
        }
        return lowest;
    }

    /**
     * The translation, in the scaled model space right after the renderer's scale, that puts a model's feet on the
     * ground. Positive moves the model down in the world (the space is Y-down). Independent of the scale, since the
     * scale is about the feet.
     *
     * @param footY the lowest leg point in model units ({@link #restFootY})
     * @return the Y translation to apply after the scale; 0 for feet at {@link #FEET_Y}
     */
    static float groundingTranslation(float footY) {
        return (FEET_Y - footY) / UNITS_PER_BLOCK;
    }

    /**
     * Moves a point from a part's frame to the model frame: the part's rotation (X, then Y, then Z, as
     * {@code ModelRenderer}), then its pivot, then the same for each parent up to the root.
     */
    private static float[] toModel(MobModelDef def, MobPartDef part, float[] v, float legPitchDegrees) {
        float[] p = v.clone();
        MobPartDef at = part;
        while (at != null) {
            float[] r = at.rotateDegrees();
            float pitch = r[0] + (at.parent() == null ? legPitchDegrees : 0.0F);
            rotate(p, pitch, r[1], r[2]);
            float[] pivot = at.pivot();
            p[0] += pivot[0];
            p[1] += pivot[1];
            p[2] += pivot[2];
            at = at.parent() == null ? null
                : def.parts()
                    .get(def.indexOf(at.parent()));
        }
        return p;
    }

    private static void rotate(float[] p, float xDeg, float yDeg, float zDeg) {
        double x = p[0];
        double y = p[1];
        double z = p[2];
        double a = Math.toRadians(xDeg);
        double y1 = y * Math.cos(a) - z * Math.sin(a);
        double z1 = y * Math.sin(a) + z * Math.cos(a);
        double b = Math.toRadians(yDeg);
        double x2 = x * Math.cos(b) + z1 * Math.sin(b);
        double z2 = -x * Math.sin(b) + z1 * Math.cos(b);
        double c = Math.toRadians(zDeg);
        double x3 = x2 * Math.cos(c) - y1 * Math.sin(c);
        double y3 = x2 * Math.sin(c) + y1 * Math.cos(c);
        p[0] = (float) x3;
        p[1] = (float) y3;
        p[2] = (float) z2;
    }
}
