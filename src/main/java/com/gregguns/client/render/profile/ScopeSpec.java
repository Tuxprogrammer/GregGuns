package com.gregguns.client.render.profile;

/**
 * Data for a gun's scope overlay: the texture and the overlay's recoil kick. Pure data. A gun with no scope has no
 * {@code ScopeSpec} (null in {@link GunTransforms}).
 *
 * <p>
 * Spec: rendering-framework.md section 9. The kick is expressed as a fraction of the screen height so the overlay
 * size does not depend on field of view (angelica-item-rendering.md).
 */
public final class ScopeSpec {

    private final String texture;
    private final double kickUp;
    private final double kickSide;

    /**
     * @param texture  resource path such as {@code gregguns:textures/fx/testscope.png}
     * @param kickUp   upward shift at full recoil, as a fraction of screen height
     * @param kickSide sideways shift at full recoil, as a fraction of screen height
     */
    public ScopeSpec(String texture, double kickUp, double kickSide) {
        this.texture = texture;
        this.kickUp = kickUp;
        this.kickSide = kickSide;
    }

    /** @return the overlay texture resource path */
    public String texture() {
        return texture;
    }

    /** @return upward kick, fraction of screen height */
    public double kickUp() {
        return kickUp;
    }

    /** @return sideways kick, fraction of screen height */
    public double kickSide() {
        return kickSide;
    }
}
