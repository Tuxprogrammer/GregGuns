package com.gregguns.network.validation;

/** A configurable {@link Sender} for tests: alive, awake, in dimension 0 at the origin by default. */
public final class FakeSender implements Sender {

    public boolean alive = true;
    public boolean spectatorLike = false;
    public int dimension = 0;
    public double x = 0;
    public double y = 0;
    public double z = 0;

    @Override
    public String name() {
        return "TestPlayer";
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public boolean isSpectatorLike() {
        return spectatorLike;
    }

    @Override
    public int dimensionId() {
        return dimension;
    }

    @Override
    public double x() {
        return x;
    }

    @Override
    public double y() {
        return y;
    }

    @Override
    public double z() {
        return z;
    }
}
