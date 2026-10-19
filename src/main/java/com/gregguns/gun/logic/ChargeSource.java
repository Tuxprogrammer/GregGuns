package com.gregguns.gun.logic;

/**
 * The energy an energy gun draws on, with no Minecraft types. The item glue adapts the stack's IC2 charge (server
 * only); an NPC or a creative player uses {@link #INFINITE}.
 *
 * <p>
 * Spec: docs/gregguns-spec/02-guns/energy-electric-items.md (GGS-02-12), docs/design/0003-energy-model.md.
 */
public interface ChargeSource {

    /** A gun with no battery: it never has charge. */
    ChargeSource NONE = new ChargeSource() {

        @Override
        public long stored() {
            return 0;
        }

        @Override
        public long drain(long eu) {
            return 0;
        }
    };

    /** Unlimited charge that discards every drain: NPCs and turrets never spend EU (GGS-02-12). */
    ChargeSource INFINITE = new ChargeSource() {

        @Override
        public long stored() {
            return Long.MAX_VALUE;
        }

        @Override
        public long drain(long eu) {
            return Math.max(0, eu);
        }
    };

    /** EU available. A pure query. */
    long stored();

    /**
     * Removes up to {@code eu} EU.
     *
     * @return how much was removed, between 0 and {@code eu}
     */
    long drain(long eu);
}
