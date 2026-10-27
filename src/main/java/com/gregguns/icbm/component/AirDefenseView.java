package com.gregguns.icbm.component;

/**
 * What the {@code gg_air_defense} component needs from an air-defense battery. The battery implements it at
 * integration, and unit tests use a fake. {@link #fireAt} must run the same checks as the battery's own automatic
 * engagement (linked radar online, the contact is in that radar's sweep and in engage range, a SAM left, cooldown), so
 * a computer never fires a shot the battery would refuse. Server thread only (GGI-01-03). No OpenComputers types.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_air_defense)
public interface AirDefenseView {

    /** @return true when a radar is linked and online */
    boolean linked();

    /** @return the SAMs loaded */
    int samCount();

    /** @return how many SAMs the battery holds at most */
    int capacity();

    /** @return true while the battery engages threats by itself */
    boolean auto();

    /** @param on true to engage by itself, false to hold fire until {@code fireAt} */
    void setAuto(boolean on);

    /**
     * @param contactId a contact id from the linked radar
     * @return ok when a SAM was launched at it, else the refusal with a short word ({@code no_link}, {@code no_sams},
     *         {@code no_contact}, {@code out_of_range}, {@code cooling}, {@code no_power}, {@code module_off})
     */
    Reply fireAt(int contactId);

    /** @return how many SAMs it has launched since it loaded */
    int fired();

    /** @return how many of those SAMs destroyed their missile since it loaded */
    int hits();
}
