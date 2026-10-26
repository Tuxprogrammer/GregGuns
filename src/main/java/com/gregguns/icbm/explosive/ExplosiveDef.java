package com.gregguns.icbm.explosive;

/**
 * One explosive: the shared registry entry every ICBM blast type is (D12). Immutable. Build with
 * {@link #builder(String, ExplosiveTier)}. No Minecraft types, both sides.
 *
 * <p>
 * Radius from fill (D23): {@code baseRadius * fill^radiusExponent}, with {@code radiusExponent} at most 1 (sub-linear
 * is allowed). The D20 fuel bonus is added by the caller through {@code IcbmBlasts.detonate}.
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     ExplosiveDef condensed = ExplosiveDef.builder("condensed", ExplosiveTier.T1)
 *         .shape(BlastShape.RAY_GRID)
 *         .baseRadius(6)
 *         .radiusExponent(0.5)
 *         .centerDamage(1200)
 *         .build();
 *     Explosives.register(condensed);
 * }
 * </pre>
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/explosive-registry.md
public final class ExplosiveDef {

    private final String id;
    private final ExplosiveTier tier;
    private final BlastShape shape;
    private final double baseRadius;
    private final double radiusExponent;
    private final int maxFill;
    private final double centerDamage;
    private final boolean annihilates;
    private final boolean annihilatesBosses;
    private final double rayEnergyValue;
    private final BlastEffect effect;
    private final boolean hidden;
    private final boolean ownsMotion;

    private ExplosiveDef(Builder b) {
        this.id = b.id;
        this.tier = b.tier;
        this.shape = b.shape;
        this.baseRadius = b.baseRadius;
        this.radiusExponent = b.radiusExponent;
        this.maxFill = b.maxFill;
        this.centerDamage = b.centerDamage;
        this.annihilates = b.annihilates;
        this.annihilatesBosses = b.annihilatesBosses;
        this.rayEnergyValue = b.rayEnergyValue;
        this.effect = b.effect;
        this.hidden = b.hidden;
        this.ownsMotion = b.ownsMotion;
    }

    /**
     * @param id   lowercase name without underscore or colon, unique, the NBT {@code explosive} value
     * @param tier the tier
     * @return a builder with a sphere shape, radius 6, exponent 0.5, no effect and no annihilation
     */
    public static Builder builder(String id, ExplosiveTier tier) {
        return new Builder(id, tier);
    }

    /** @return the registry id, also the warhead NBT {@code explosive} value */
    public String id() {
        return id;
    }

    /** @return the tier */
    public ExplosiveTier tier() {
        return tier;
    }

    /** @return how terrain is removed */
    public BlastShape shape() {
        return shape;
    }

    /** @return the largest fill any casing may carry for this type (a casing may cap lower, D23) */
    public int maxFill() {
        return maxFill;
    }

    /** @return damage at the blast center, half hearts; the ramp falls to zero at the radius (D34) */
    public double centerDamage() {
        return centerDamage;
    }

    /** @return true when the kill zone annihilates every living entity (nuclear and up, D30) */
    public boolean annihilates() {
        return annihilates;
    }

    /** @return true when annihilation includes bosses; false keeps the Nuke Launcher's half-health rule */
    public boolean annihilatesBosses() {
        return annihilatesBosses;
    }

    /** @return the energy a shape's rays start with (see blast-engine.md); 0 means derived from the radius */
    public double rayEnergy() {
        return rayEnergyValue;
    }

    /** @return the per-type effect, never null */
    public BlastEffect effect() {
        return effect;
    }

    /** @return true when the type's effect moves entities itself, so the engine adds no outward knockback */
    public boolean ownsMotion() {
        return ownsMotion;
    }

    /** @return true for a test-only explosive: registered and detonable, but left out of {@link Explosives#all()} */
    public boolean hidden() {
        return hidden;
    }

    /**
     * The damage at the blast center for a fill: {@code centerDamage * sqrt(fill / maxFill)}, so a full casing meets
     * the D34 target and a partial fill deals less. The ramp falls to zero at the radius.
     *
     * @param fill the warhead fill, clamped to 1..{@link #maxFill()}
     * @return damage at the center, half hearts
     */
    public double damage(int fill) {
        int f = Math.max(1, Math.min(fill, maxFill));
        return centerDamage * Math.sqrt((double) f / maxFill);
    }

    /**
     * @param fill the warhead fill, clamped to 1..{@link #maxFill()}
     * @return the blast radius in blocks, before any fuel bonus
     */
    public double radius(int fill) {
        int f = Math.max(1, Math.min(fill, maxFill));
        return baseRadius * Math.pow(f, radiusExponent);
    }

    /** Builder for {@link ExplosiveDef}. */
    public static final class Builder {

        private final String id;
        private final ExplosiveTier tier;
        private BlastShape shape = BlastShape.RAY_SPHERE;
        private double baseRadius = 6.0;
        private double radiusExponent = 0.5;
        private int maxFill = 128;
        private double centerDamage;
        private boolean annihilates;
        private boolean annihilatesBosses;
        private double rayEnergyValue;
        private BlastEffect effect = BlastEffect.NONE;
        private boolean hidden;
        private boolean ownsMotion;

        private Builder(String id, ExplosiveTier tier) {
            if (id == null || id.isEmpty()
                || id.indexOf('_') >= 0
                || id.indexOf(':') >= 0
                || !id.equals(id.toLowerCase(java.util.Locale.ROOT))) {
                throw new IllegalArgumentException("explosive id must be lowercase, no '_' or ':': " + id);
            }
            this.id = id;
            this.tier = java.util.Objects.requireNonNull(tier);
            this.centerDamage = tier.minCenterDamage() == Double.POSITIVE_INFINITY ? 0.0 : tier.minCenterDamage();
        }

        /** @param v the shape @return this */
        public Builder shape(BlastShape v) {
            this.shape = java.util.Objects.requireNonNull(v);
            return this;
        }

        /** @param v radius at fill 1, blocks, above 0 @return this */
        public Builder baseRadius(double v) {
            if (!(v > 0.0)) {
                throw new IllegalArgumentException("baseRadius must be positive");
            }
            this.baseRadius = v;
            return this;
        }

        /** @param v exponent over fill, 0 to 1 @return this */
        public Builder radiusExponent(double v) {
            if (v < 0.0 || v > 1.0) {
                throw new IllegalArgumentException("radiusExponent must be 0..1");
            }
            this.radiusExponent = v;
            return this;
        }

        /** @param v most fill, at least 1 @return this */
        public Builder maxFill(int v) {
            this.maxFill = Math.max(1, v);
            return this;
        }

        /** @param v center damage; defaults to the tier's D34 minimum @return this */
        public Builder centerDamage(double v) {
            this.centerDamage = v;
            return this;
        }

        /**
         * @param bosses whether bosses die too (true for nuclear and up, D30)
         * @return this, with annihilation on
         */
        public Builder annihilating(boolean bosses) {
            this.annihilates = true;
            this.annihilatesBosses = bosses;
            return this;
        }

        /** @param v starting ray energy, 0 for the shape's default @return this */
        public Builder rayEnergy(double v) {
            this.rayEnergyValue = v;
            return this;
        }

        /** @param v the per-type effect @return this */
        public Builder effect(BlastEffect v) {
            this.effect = java.util.Objects.requireNonNull(v);
            return this;
        }

        /** @return this, marked as moving entities in its own effect; the engine then skips its knockback */
        public Builder ownsMotion() {
            this.ownsMotion = true;
            return this;
        }

        /** @return this, marked test-only (scenarios); the registry's {@code all()} skips it */
        public Builder hidden() {
            this.hidden = true;
            return this;
        }

        /** @return the immutable definition */
        public ExplosiveDef build() {
            return new ExplosiveDef(this);
        }
    }
}
