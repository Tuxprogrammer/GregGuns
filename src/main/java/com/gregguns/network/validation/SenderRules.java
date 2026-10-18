package com.gregguns.network.validation;

import java.util.function.Predicate;

/**
 * {@link Rules} plus the checks that depend on who sent the packet: dimension, reach and permission. Only
 * server-bound packets get one of these.
 *
 * Side: server.
 */
public final class SenderRules extends Rules<SenderRules> {

    /** Default reach for block and tile targets, in blocks (GGS-01-09). */
    public static final double DEFAULT_REACH_BLOCKS = 8.0;

    private final Sender sender;

    /** Creates an empty rule set for the given sender. */
    public SenderRules(Sender sender) {
        this.sender = sender;
    }

    @Override
    protected SenderRules self() {
        return this;
    }

    /** Returns the sender these rules check. */
    public Sender sender() {
        return sender;
    }

    /**
     * Requires the target dimension to be the sender's current one (CORE-06, CORE-08).
     *
     * @param targetDimensionId the dimension id the packet names
     */
    public SenderRules sameDimension(int targetDimensionId) {
        custom(
            () -> targetDimensionId != sender.dimensionId() ? new Violation(
                Rejection.WRONG_DIMENSION,
                "target dimension " + targetDimensionId + ", sender in " + sender.dimensionId()) : null);
        return this;
    }

    /**
     * Requires a block within {@link #DEFAULT_REACH_BLOCKS} of the sender.
     *
     * @param x block x
     * @param y block y
     * @param z block z
     */
    public SenderRules withinReachOfBlock(int x, int y, int z) {
        return withinReachOfBlock(x, y, z, DEFAULT_REACH_BLOCKS);
    }

    /**
     * Requires a block within {@code reachBlocks} of the sender, measured to the block centre.
     *
     * @param x           block x
     * @param y           block y
     * @param z           block z
     * @param reachBlocks the maximum distance in blocks
     */
    public SenderRules withinReachOfBlock(int x, int y, int z, double reachBlocks) {
        return withinReachOf(x + 0.5, y + 0.5, z + 0.5, reachBlocks);
    }

    /**
     * Requires a point within {@code reachBlocks} of the sender. NaN coordinates are rejected.
     *
     * @param x           point x
     * @param y           point y
     * @param z           point z
     * @param reachBlocks the maximum distance in blocks, greater than zero
     */
    public SenderRules withinReachOf(double x, double y, double z, double reachBlocks) {
        if (!(reachBlocks > 0)) {
            throw new IllegalArgumentException("reachBlocks must be positive: " + reachBlocks);
        }
        custom(() -> {
            double dx = x - sender.x();
            double dy = y - sender.y();
            double dz = z - sender.z();
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            // Negated <= so that NaN fails.
            return !(distanceSquared <= reachBlocks * reachBlocks)
                ? new Violation(Rejection.OUT_OF_REACH, "distance^2 " + distanceSquared + " > " + reachBlocks + "^2")
                : null;
        });
        return this;
    }

    /**
     * Requires an arbitrary permission, for ownership and team checks that later milestones add.
     *
     * @param what      what is being permitted, for the log
     * @param permitted returns {@code true} when the sender may act
     */
    public SenderRules authorised(String what, Predicate<Sender> permitted) {
        custom(() -> permitted.test(sender) ? null : new Violation(Rejection.UNAUTHORISED, "not permitted: " + what));
        return this;
    }

    /** Requires the sender alive and not spectator-like. {@code ServerboundPacket} always applies this first. */
    public SenderRules senderMayAct() {
        custom(() -> !sender.isAlive() ? new Violation(Rejection.SENDER_INACTIVE, "sender is dead or gone") : null);
        custom(
            () -> sender.isSpectatorLike() ? new Violation(Rejection.SENDER_SPECTATOR, "sender is spectator-like")
                : null);
        return this;
    }
}
