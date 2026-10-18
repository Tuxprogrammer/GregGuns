package com.gregguns.gun.net;

/**
 * What a {@link PacketGunEvent} reports. The ordinal is the wire value, so append new kinds at the end only.
 */
public enum GunEventKind {

    /** A shot was fired; the argument is the hand for akimbo guns (0 right, 1 left). Others only. */
    SHOT_FIRED,
    /** A reload began; the argument is its duration in ticks. */
    RELOAD_STARTED,
    /** A reload was cancelled or refused. The argument is unused. */
    RELOAD_ENDED,
    /** The shooter's zoom changed; the argument is 1 for zoomed, 0 for not. The shooter only. */
    ZOOM
}
