package com.gregguns.client.mob;

/** Pure math for the mob renderers' vertical offset, kept free of Minecraft classes so tests can run headless. */
final class MobYOffset {

    private MobYOffset() {}

    /**
     * The translation to apply in the already-scaled model space so the model ends up {@code yOffset} blocks higher in
     * world space. Called right after {@code glScalef(scale, scale, scale)}, where one model unit is {@code scale}
     * blocks, so the translation is the offset divided by the scale.
     *
     * @param yOffset the wanted lift in blocks; positive is up, negative is down
     * @param scale   the uniform scale already applied, greater than zero
     * @return the Y translation in scaled model units
     */
    static float scaledTranslation(float yOffset, float scale) {
        return yOffset / scale;
    }
}
