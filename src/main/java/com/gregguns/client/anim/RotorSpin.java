package com.gregguns.client.anim;

/**
 * The spin of a gun's rotor (the Minigun's barrel cluster) as a pure state machine: an angular speed and a phase,
 * advanced once per client tick and read back interpolated by the partial tick. While the shooter fires, the speed
 * ramps up in a straight line to full speed; when firing stops, it coasts down in a straight line and the rotor comes
 * to rest at angle 0.
 *
 * <p>
 * Units: phase in turns (1.0 is 360 degrees), speed in turns per tick.
 *
 * <p>
 * Settling at rest. The rotor looks the same every {@link Spec#symmetryTurns} (the Minigun's six barrels repeat every
 * 60 degrees, but its clamp discs only every 180, so the whole rotor repeats every half turn). A coast would
 * naturally stop at any angle, and snapping to the nearest rest angle would jump by up to a quarter turn. So the
 * coast chooses its deceleration so that it stops exactly on a symmetric angle, the nearest to where the nominal
 * coast would end; that stretches or shrinks a 20 tick coast by about two ticks. At rest the phase is exactly 0.
 *
 * <p>
 * Spec: docs/techguns-spec/03-gun-visuals-audio/model-notes/minigun.md ("Spin-up and spin-down" suggests the ramp
 * lengths; the original has none); docs/gregguns-spec/02-guns/index.md (Minigun, recorded deviation).
 */
public final class RotorSpin {

    /** The numbers of one rotor: all positive. */
    public static final class Spec {

        final double fullSpeed;
        final double upTicks;
        final double downTicks;
        final double symmetryTurns;

        /**
         * @param fullSpeed     full speed, turns per tick
         * @param upTicks       ticks from rest to full speed
         * @param downTicks     ticks from full speed to rest (nominal; see the class comment)
         * @param symmetryTurns the angle, in turns, after which the rotor looks identical; must divide 1
         */
        public Spec(double fullSpeed, double upTicks, double downTicks, double symmetryTurns) {
            if (fullSpeed <= 0 || upTicks <= 0 || downTicks <= 0 || symmetryTurns <= 0) {
                throw new IllegalArgumentException("rotor spin numbers must be positive");
            }
            this.fullSpeed = fullSpeed;
            this.upTicks = upTicks;
            this.downTicks = downTicks;
            this.symmetryTurns = symmetryTurns;
        }

        /** @return a fresh spin at rest */
        public RotorSpin create() {
            return new RotorSpin(this);
        }
    }

    private final Spec spec;
    private double speed;
    private double phase;
    private double prevPhase;
    private boolean coasting;
    private double coastDecel;
    private double coastTarget;

    private RotorSpin(Spec spec) {
        this.spec = spec;
    }

    /** @return true when stopped at angle 0 (nothing to draw or advance) */
    public boolean atRest() {
        return speed == 0.0 && phase == 0.0 && prevPhase == 0.0;
    }

    /** @return the current speed, turns per tick */
    public double speed() {
        return speed;
    }

    /** Stops dead at angle 0 (the gun was put away, or a reload began). */
    public void reset() {
        speed = 0.0;
        phase = 0.0;
        prevPhase = 0.0;
        coasting = false;
    }

    /**
     * Advances one client tick.
     *
     * @param driving true if the shooter is firing this tick
     */
    public void tick(boolean driving) {
        prevPhase = phase;
        if (driving) {
            coasting = false;
            double next = Math.min(spec.fullSpeed, speed + spec.fullSpeed / spec.upTicks);
            phase += (speed + next) / 2.0;
            speed = next;
        } else if (speed > 0.0) {
            if (!coasting) {
                beginCoast();
            }
            double next = speed - coastDecel;
            if (next <= 0.0) {
                // The last, partial tick: it covers exactly the rest of the distance to the target.
                phase = coastTarget;
                speed = 0.0;
                coasting = false;
                shift(coastTarget);
                return;
            }
            phase += (speed + next) / 2.0;
            speed = next;
        }
        if (!coasting) {
            shift(Math.floor(phase)); // keep the numbers small; whole turns look the same
        }
    }

    /**
     * @param partial the partial tick of the frame, 0 to 1
     * @return the rotor angle in turns, 0 to below 1, interpolated between the last two ticks
     */
    public double turns(double partial) {
        double v = prevPhase + (phase - prevPhase) * partial;
        return v - Math.floor(v);
    }

    private void beginCoast() {
        shift(Math.floor(phase));
        double s = speed * speed / (2.0 * spec.fullSpeed / spec.downTicks);
        double sym = spec.symmetryTurns;
        double target = Math.round((phase + s) / sym) * sym;
        // Never stop in under half the nominal distance: that would be a jerk, not a coast.
        while (target - phase < s * 0.5 || target - phase <= 1e-9) {
            target += sym;
        }
        coastTarget = target;
        coastDecel = speed * speed / (2.0 * (target - phase));
        coasting = true;
    }

    /** Moves the origin of the phase by {@code by} turns (a symmetric angle, so the look is unchanged). */
    private void shift(double by) {
        phase -= by;
        prevPhase -= by;
        if (coasting) {
            coastTarget -= by;
        }
        if (speed == 0.0 && !coasting && Math.abs(phase) < 1e-12) {
            phase = 0.0; // rest is exactly angle 0
        }
    }
}
