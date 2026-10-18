package com.gregguns.machine.reaction;

/**
 * A machine that runs reactions with a player-set intensity: implemented by the single-block chamber and the Retort so
 * they share one GUI control and one set of rules. Server side owns the values; the GUI reads them through sync
 * values.
 */
// Spec: docs/gregguns-spec/08-machines-production/modularui2-guis.md (GGS-08-28)
public interface ReactionControlled {

    /** @return the intensity setting, 0 to 10 */
    int getIntensity();

    /**
     * Sets the intensity from a (possibly client-written) value; the implementation clamps it to 0 to 10.
     *
     * @param value the requested setting
     */
    void setIntensity(int value);

    /** @return the run's state machine */
    ReactionState getReactionState();
}
