package com.gregguns.client.render.profile;

/**
 * Everything the renderer needs to know about one gun's look that is data rather than code: timings, the transform
 * rows of the gun sheet, the muzzle flash and the optional scope. Pure and immutable.
 *
 * <p>
 * Rows (spec names): first-person base, idle, fire and reload; third-person base, aim extra translate, recoil and
 * reload; dropped (ground) and inventory (icon). Each is a {@link TransformProgram}. Fire and recoil rows receive the
 * recoil progress, reload rows the reload progress, the rest 0.
 *
 * <p>
 * Spec: docs/techguns-spec/03-gun-visuals-audio/gun-sheets.md (row legend) and rendering-framework.md sections 4 to 6.
 */
public final class GunTransforms {

    private final int partCount;
    private final int recoilTicks;
    private final int flashTicks;
    private final int reloadTicks;
    private final TransformProgram fpBase;
    private final TransformProgram fpIdle;
    private final TransformProgram fpFire;
    private final TransformProgram fpReload;
    private final TransformProgram tpBase;
    private final double[] tpAim;
    private final TransformProgram tpRecoil;
    private final TransformProgram tpReload;
    private final TransformProgram ground;
    private final TransformProgram icon;
    private final MuzzleFlashSpec muzzleFlash;
    private final ScopeSpec scope;
    private final PartVisibility partVisibility;

    /**
     * Decides whether a model part is drawn this frame, from the state a model may see. Used for parts that appear
     * and disappear with the ammo (a loaded rocket's nose). Pure.
     */
    @FunctionalInterface
    public interface PartVisibility {

        /** Every part always drawn. */
        PartVisibility ALWAYS = (part, ammoLeft, reloadProgress) -> true;

        /**
         * @param part           part index
         * @param ammoLeft       rounds loaded; 100 when unknown
         * @param reloadProgress reload progress 0 to 1, or any value above 1 when not reloading
         * @return true to draw the part
         */
        boolean visible(int part, int ammoLeft, float reloadProgress);
    }

    private GunTransforms(Builder b) {
        partCount = b.partCount;
        recoilTicks = b.recoilTicks;
        flashTicks = b.flashTicks;
        reloadTicks = b.reloadTicks;
        fpBase = b.fpBase;
        fpIdle = b.fpIdle;
        fpFire = b.fpFire;
        fpReload = b.fpReload;
        tpBase = b.tpBase;
        tpAim = b.tpAim.clone();
        tpRecoil = b.tpRecoil;
        tpReload = b.tpReload;
        ground = b.ground;
        icon = b.icon;
        muzzleFlash = b.muzzleFlash;
        scope = b.scope;
        partVisibility = b.partVisibility;
    }

    /** @return a new builder; every row defaults to no steps */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Replays the third-person aimed pre-rotation: rotX(-45), rotY(-18), rotZ(45), rotY(45), T(0, -1.3, -0.4), the
     * gun's extra aim translate, rotY(-45). Applied to every player holding a gun (framework section 6.3).
     */
    public void applyAimed(TransformProgram.OpSink sink) {
        sink.rotate(-45.0, 1, 0, 0);
        sink.rotate(-18.0, 0, 1, 0);
        sink.rotate(45.0, 0, 0, 1);
        sink.rotate(45.0, 0, 1, 0);
        sink.translate(0.0, -1.3, -0.4);
        sink.translate(tpAim[0], tpAim[1], tpAim[2]);
        sink.rotate(-45.0, 0, 1, 0);
    }

    /** @return how many model parts are drawn per frame, each in its own matrix */
    public int partCount() {
        return partCount;
    }

    /** @return recoil length in ticks */
    public int recoilTicks() {
        return recoilTicks;
    }

    /** @return muzzle flash length in ticks */
    public int flashTicks() {
        return flashTicks;
    }

    /** @return reload length in ticks, the default for animations when the item gives none */
    public int reloadTicks() {
        return reloadTicks;
    }

    /** @return first-person base row */
    public TransformProgram fpBase() {
        return fpBase;
    }

    /** @return first-person idle row */
    public TransformProgram fpIdle() {
        return fpIdle;
    }

    /** @return first-person firing row (progress = recoil) */
    public TransformProgram fpFire() {
        return fpFire;
    }

    /** @return first-person reload row (progress = reload) */
    public TransformProgram fpReload() {
        return fpReload;
    }

    /** @return third-person base row */
    public TransformProgram tpBase() {
        return tpBase;
    }

    /** @return third-person recoil row (progress = recoil) */
    public TransformProgram tpRecoil() {
        return tpRecoil;
    }

    /** @return third-person reload row (progress = reload) */
    public TransformProgram tpReload() {
        return tpReload;
    }

    /** @return dropped-item row */
    public TransformProgram ground() {
        return ground;
    }

    /** @return inventory row */
    public TransformProgram icon() {
        return icon;
    }

    /** @return the muzzle flash, or null for none */
    public MuzzleFlashSpec muzzleFlash() {
        return muzzleFlash;
    }

    /** @return the scope overlay, or null for a gun without one */
    public ScopeSpec scope() {
        return scope;
    }

    /**
     * @return whether the given part is drawn for this ammo and reload state (always true unless the gun sets a rule)
     */
    public boolean partVisible(int part, int ammoLeft, float reloadProgress) {
        return partVisibility.visible(part, ammoLeft, reloadProgress);
    }

    /** Builds {@link GunTransforms}. */
    public static final class Builder {

        private int partCount = 1;
        private int recoilTicks = 5;
        private int flashTicks = 5;
        private int reloadTicks = 30;
        private TransformProgram fpBase = TransformProgram.EMPTY;
        private TransformProgram fpIdle = TransformProgram.EMPTY;
        private TransformProgram fpFire = TransformProgram.EMPTY;
        private TransformProgram fpReload = TransformProgram.EMPTY;
        private TransformProgram tpBase = TransformProgram.EMPTY;
        private double[] tpAim = { 0, 0, 0 };
        private TransformProgram tpRecoil = TransformProgram.EMPTY;
        private TransformProgram tpReload = TransformProgram.EMPTY;
        private TransformProgram ground = TransformProgram.EMPTY;
        private TransformProgram icon = TransformProgram.EMPTY;
        private MuzzleFlashSpec muzzleFlash;
        private ScopeSpec scope;
        private PartVisibility partVisibility = PartVisibility.ALWAYS;

        private Builder() {}

        /** A rule that hides parts by ammo and reload state; the default draws every part. */
        public Builder partVisibility(PartVisibility rule) {
            partVisibility = rule;
            return this;
        }

        /** Number of parts drawn per frame. */
        public Builder parts(int count) {
            partCount = count;
            return this;
        }

        /** Window lengths in ticks. */
        public Builder timing(int recoil, int flash, int reload) {
            recoilTicks = recoil;
            flashTicks = flash;
            reloadTicks = reload;
            return this;
        }

        /** First-person rows. */
        public Builder firstPerson(TransformProgram base, TransformProgram idle, TransformProgram fire,
            TransformProgram reload) {
            fpBase = base;
            fpIdle = idle;
            fpFire = fire;
            fpReload = reload;
            return this;
        }

        /** Third-person rows. */
        public Builder thirdPerson(TransformProgram base, double[] aimTranslate, TransformProgram recoil,
            TransformProgram reload) {
            tpBase = base;
            tpAim = aimTranslate.clone();
            tpRecoil = recoil;
            tpReload = reload;
            return this;
        }

        /** Dropped and inventory rows. */
        public Builder world(TransformProgram groundRow, TransformProgram iconRow) {
            ground = groundRow;
            icon = iconRow;
            return this;
        }

        /** The muzzle flash. */
        public Builder muzzleFlash(MuzzleFlashSpec spec) {
            muzzleFlash = spec;
            return this;
        }

        /** The scope overlay. */
        public Builder scope(ScopeSpec spec) {
            scope = spec;
            return this;
        }

        /** @return the finished data */
        public GunTransforms build() {
            return new GunTransforms(this);
        }
    }
}
