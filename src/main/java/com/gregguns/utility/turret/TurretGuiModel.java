package com.gregguns.utility.turret;

import java.util.Locale;
import java.util.function.Function;

import com.gregguns.utility.security.SecurityLevel;

/**
 * What the Turret screen says and which of its controls are live, as pure functions and one small state holder. The
 * server fills a {@link State} through sync values and the client copies it, so every label, color class and locked
 * flag is decided here and unit tested without a world or a GUI. The server never trusts this: the button handlers in
 * {@link MTETurret} re-check the pressing player.
 */
// Spec: docs/gregguns-spec/09-machines-utility/turret.md (GUI)
public final class TurretGuiModel {

    /** Flag bit: the base is repairing a dead turret. */
    public static final int F_DEAD = 1;
    /** Flag bit: the redstone mode allows work. */
    public static final int F_ENABLED = 2;
    /** Flag bit: the turret's AI is on (powered). */
    public static final int F_ACTIVE = 4;
    /** Flag bit: a live turret entity is linked. */
    public static final int F_TURRET = 8;

    /** Prefix of a player name in the target string; any other text is an entity lang key. */
    public static final String PLAYER_PREFIX = "@";

    private TurretGuiModel() {}

    /** How the status reads: a color class for the screen. */
    public enum Tone {
        /** Working as intended. */
        GOOD,
        /** Not working for a reason the player can fix. */
        WARN,
        /** Broken or out of power. */
        BAD
    }

    /** The one-word state of the turret. */
    public enum Status {

        /** The base is rebuilding a dead turret. */
        REPAIRING("repairing", Tone.BAD),
        /** No live turret is linked (spawning, or no room above the base). */
        NO_TURRET("no_turret", Tone.WARN),
        /** The redstone mode switches the turret off right now. */
        REDSTONE_OFF("redstone_off", Tone.WARN),
        /** The base has no EU for the upkeep. */
        NO_POWER("no_power", Tone.BAD),
        /** Powered and enabled but nothing is mounted. */
        NO_GUN("no_gun", Tone.WARN),
        /** On guard. */
        GUARDING("guarding", Tone.GOOD);

        private final String key;
        private final Tone tone;

        Status(String key, Tone tone) {
            this.key = key;
            this.tone = tone;
        }

        /** @return the lang key of the state word */
        public String langKey() {
            return "gregguns.turret.gui.status." + key;
        }

        /** @return the color class */
        public Tone tone() {
            return tone;
        }
    }

    /** The values the screen displays; plain fields so a sync value can write them from the network thread. */
    public static final class State {

        /** {@code F_*} bits. */
        public volatile int flags;
        /** EU in the buffer. */
        public volatile long eu;
        /** Raw target string: empty for idle, {@code @name} for a player, else an entity lang key. */
        public volatile String target = "";
        /** The owner's name; empty for an ownerless block. */
        public volatile String owner = "";
        /** True when the viewer may use the Turret (redstone and animal buttons). */
        public volatile boolean canUse = true;
        /** True when the viewer may change the security level (the owner). */
        public volatile boolean canSecurity;
    }

    /**
     * @param dead    whether the turret is dead and being repaired
     * @param enabled whether the redstone mode allows work
     * @param active  whether the turret's AI is on
     * @param turret  whether a live turret entity is linked
     * @return the packed flags
     */
    public static int flags(boolean dead, boolean enabled, boolean active, boolean turret) {
        return (dead ? F_DEAD : 0) | (enabled ? F_ENABLED : 0) | (active ? F_ACTIVE : 0) | (turret ? F_TURRET : 0);
    }

    /**
     * @param flags  the packed {@code F_*} bits
     * @param hasGun whether a mountable gun sits in the gun slot
     * @return the state; the first matching of repairing, no turret, redstone off, no power, no gun, guarding
     */
    public static Status status(int flags, boolean hasGun) {
        if ((flags & F_DEAD) != 0) {
            return Status.REPAIRING;
        }
        if ((flags & F_TURRET) == 0) {
            return Status.NO_TURRET;
        }
        if ((flags & F_ENABLED) == 0) {
            return Status.REDSTONE_OFF;
        }
        if ((flags & F_ACTIVE) == 0) {
            return Status.NO_POWER;
        }
        return hasGun ? Status.GUARDING : Status.NO_GUN;
    }

    /**
     * @param id a redstone mode id
     * @return the lang key of the button's value text
     */
    public static String redstoneKey(int id) {
        switch (RedstoneMode.fromId(id)) {
            case WITH_SIGNAL:
                return "gregguns.turret.gui.redstone.signal";
            case WITHOUT_SIGNAL:
                return "gregguns.turret.gui.redstone.no_signal";
            default:
                return "gregguns.turret.gui.redstone.ignore";
        }
    }

    /**
     * @param id a security level id
     * @return the lang key of the button's value text
     */
    public static String securityKey(int id) {
        switch (SecurityLevel.fromId(id)) {
            case FRIENDS:
                return "gregguns.turret.gui.security.friends";
            case OWNER:
                return "gregguns.turret.gui.security.owner";
            default:
                return "gregguns.turret.gui.security.public";
        }
    }

    /**
     * @param live      whether the viewer may press the button
     * @param ownerOnly true for the security button, false for the others
     * @return the lang key of the reason the button is locked, or null when it is live
     */
    public static String lockedKey(boolean live, boolean ownerOnly) {
        if (live) {
            return null;
        }
        return ownerOnly ? "gregguns.turret.gui.locked_owner" : "gregguns.turret.gui.locked_use";
    }

    /**
     * @param raw       the synced target string
     * @param translate turns an entity lang key into text
     * @return the target's display name, or null when idle
     */
    public static String targetDisplay(String raw, Function<String, String> translate) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        return raw.startsWith(PLAYER_PREFIX) ? raw.substring(PLAYER_PREFIX.length()) : translate.apply(raw);
    }

    /**
     * @param value a number of EU
     * @return the number with thousands separators, "0" for a negative
     */
    public static String eu(long value) {
        return String.format(Locale.US, "%,d", Math.max(0L, value));
    }

    /**
     * @param stored   EU held
     * @param capacity EU the buffer holds
     * @return fill from 0 to 1; 0 for no capacity
     */
    public static float euFraction(long stored, long capacity) {
        if (capacity <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (float) stored / capacity));
    }
}
