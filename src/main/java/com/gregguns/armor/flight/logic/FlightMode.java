package com.gregguns.armor.flight.logic;

/** What a back device does to the wearer's motion this tick. Plain Java, both sides. */
public enum FlightMode {
    /** Leave the motion alone. */
    NONE,
    /** Convert fall speed into forward speed (the Glider). */
    GLIDE,
    /** Hover and fly under power (the Anti Gravity Device). */
    FLY
}
