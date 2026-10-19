package com.gregguns.client.render.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A short, ordered list of translate, rotate and scale steps, written in GL call order (each later step applies to
 * the model first), whose values may depend on a progress {@code p} and a part index. It is the data form of the
 * per-gun transform rows in the gun sheets. Pure: {@link #apply} replays the steps into an {@link OpSink}, which the
 * renderer backs with {@code GL11} and tests back with a recorder.
 *
 * <p>
 * Spec: docs/techguns-spec/03-gun-visuals-audio/rendering-framework.md section 5 (notation) and gun-sheets.md.
 */
public final class TransformProgram {

    /** No steps. */
    public static final TransformProgram EMPTY = new TransformProgram(new Step[0]);

    /** A value that may depend on progress and the part index. */
    @FunctionalInterface
    public interface Term {

        /**
         * @param p    progress of the relevant window, 0 to 1
         * @param part model part index being drawn
         * @return the value
         */
        double at(double p, int part);
    }

    /** Receives the resolved operations in order. */
    public interface OpSink {

        /** Translate by blocks. */
        void translate(double x, double y, double z);

        /** Rotate by degrees about an axis. */
        void rotate(double degrees, double x, double y, double z);

        /** Scale. */
        void scale(double x, double y, double z);
    }

    private enum Kind {
        TRANSLATE,
        ROTATE,
        SCALE
    }

    private static final class Step {

        final Kind kind;
        final Term a;
        final Term x;
        final Term y;
        final Term z;

        Step(Kind kind, Term a, Term x, Term y, Term z) {
            this.kind = kind;
            this.a = a;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private final Step[] steps;

    private TransformProgram(Step[] steps) {
        this.steps = steps;
    }

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    /** A constant term. */
    public static Term c(double value) {
        return (p, part) -> value;
    }

    /**
     * Replays every step into {@code sink}.
     *
     * @param p    progress passed to every term
     * @param part part index passed to every term
     * @param sink where the resolved operations go
     */
    public void apply(double p, int part, OpSink sink) {
        for (Step s : steps) {
            switch (s.kind) {
                case TRANSLATE:
                    sink.translate(s.x.at(p, part), s.y.at(p, part), s.z.at(p, part));
                    break;
                case ROTATE:
                    sink.rotate(s.a.at(p, part), s.x.at(p, part), s.y.at(p, part), s.z.at(p, part));
                    break;
                default:
                    sink.scale(s.x.at(p, part), s.y.at(p, part), s.z.at(p, part));
                    break;
            }
        }
    }

    /** @return number of steps */
    public int size() {
        return steps.length;
    }

    /** Builds a {@link TransformProgram}. */
    public static final class Builder {

        private final List<Step> list = new ArrayList<>();

        private Builder() {}

        /** Translate by constants. */
        public Builder translate(double x, double y, double z) {
            return translate(c(x), c(y), c(z));
        }

        /** Translate by terms. */
        public Builder translate(Term x, Term y, Term z) {
            list.add(new Step(Kind.TRANSLATE, null, x, y, z));
            return this;
        }

        /** Rotate about X by a constant. */
        public Builder rotX(double degrees) {
            return rotX(c(degrees));
        }

        /** Rotate about X by a term. */
        public Builder rotX(Term degrees) {
            return rotate(degrees, 1, 0, 0);
        }

        /** Rotate about Y by a constant. */
        public Builder rotY(double degrees) {
            return rotY(c(degrees));
        }

        /** Rotate about Y by a term. */
        public Builder rotY(Term degrees) {
            return rotate(degrees, 0, 1, 0);
        }

        /** Rotate about Z by a constant. */
        public Builder rotZ(double degrees) {
            return rotZ(c(degrees));
        }

        /** Rotate about Z by a term. */
        public Builder rotZ(Term degrees) {
            return rotate(degrees, 0, 0, 1);
        }

        private Builder rotate(Term degrees, double x, double y, double z) {
            list.add(new Step(Kind.ROTATE, degrees, c(x), c(y), c(z)));
            return this;
        }

        /** Uniform scale. */
        public Builder scale(double s) {
            list.add(new Step(Kind.SCALE, null, c(s), c(s), c(s)));
            return this;
        }

        /** Uniform scale by a term, which may depend on the part (a part scaled to 0 is not drawn). */
        public Builder scale(Term s) {
            list.add(new Step(Kind.SCALE, null, s, s, s));
            return this;
        }

        /** "About pivot P": translate to the pivot, run {@code body}, translate back. */
        public Builder about(double px, double py, double pz, Consumer<Builder> body) {
            translate(px, py, pz);
            body.accept(this);
            return translate(-px, -py, -pz);
        }

        /** @return the finished program */
        public TransformProgram build() {
            return new TransformProgram(list.toArray(new Step[0]));
        }
    }
}
