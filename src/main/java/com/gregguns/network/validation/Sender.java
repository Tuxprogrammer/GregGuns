package com.gregguns.network.validation;

/**
 * The facts about a packet's sender that validation needs, with no Minecraft types so JUnit can supply a fake.
 * {@code com.gregguns.network.PlayerSender} is the real implementation.
 *
 * Side: server.
 */
public interface Sender {

    /** Returns the sender's name, for logs. */
    String name();

    /** Returns {@code true} while the sender is alive and connected. */
    boolean isAlive();

    /** Returns {@code true} when the sender is in a spectator-equivalent state (1.7.10 has no spectator mode). */
    boolean isSpectatorLike();

    /** Returns the id of the dimension the sender is currently in. */
    int dimensionId();

    /** Returns the sender's x coordinate. */
    double x();

    /** Returns the sender's y coordinate (feet). */
    double y();

    /** Returns the sender's z coordinate. */
    double z();
}
