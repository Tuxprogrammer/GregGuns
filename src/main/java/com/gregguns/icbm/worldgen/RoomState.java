package com.gregguns.icbm.worldgen;

/**
 * How lived-in one room of a silo complex is (D309, D317). The complex's {@link ActivityLevel} sets it room by room
 * through {@code ComplexContents.roomState}: an operational complex is staffed throughout, an abandoned one gutted
 * throughout, and a partly abandoned one has rooms in service (lit) beside mothballed ones (dark). Lamps, wear,
 * machines and chest loot all read the same state, so a room reads one way in every populate window.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md (Room state); D309, D317
public enum RoomState {

    /** An operational complex's room: lit, working, fully stocked. */
    STAFFED,
    /** A partly abandoned complex's lit room: still worked, well stocked. */
    IN_SERVICE,
    /** A partly abandoned complex's dark room: shut down, thin stock, some junk and spoiled food. */
    MOTHBALLED,
    /** An abandoned complex's room: picked over, mostly junk, worn gear, spoiled food. */
    GUTTED
}
