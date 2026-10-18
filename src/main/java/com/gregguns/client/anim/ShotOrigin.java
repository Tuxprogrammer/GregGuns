package com.gregguns.client.anim;

/** Where a "shot fired" notification came from. Decides whether it may restart a running recoil. */
public enum ShotOrigin {
    /** The local player's own shot, played the moment they pulled the trigger. */
    PREDICTED,
    /** A "player X fired" packet for someone else (or an NPC). Always starts the animation. */
    REMOTE,
    /**
     * The server's echo of the local player's own shot. Ignored when a predicted shot started recently, so the
     * first-person kick does not restart with latency (GVA-03).
     */
    SERVER_ECHO
}
