package com.gregguns.core.testing;

/**
 * The text protocol for a dev-only client to report what it sees back to the server: the reverse of
 * {@link AutotestSignal}. The client sends an ordinary chat line that starts with {@link #PREFIX}; a dev-only server
 * handler swallows it and files it by viewer, owner and slot. Pure text handling, no Minecraft types. Both sides, dev
 * only.
 *
 * <p>
 * A line is {@code PREFIX viewer owner slot payload}, with no spaces inside any field. The vanilla server kicks a
 * player whose chat line is longer than {@link #MAX_LINE} characters, so {@link #format} refuses to build a longer one.
 */
public final class AutotestReport {

    /** Chat-line prefix that marks a report; short, because chat lines are capped at 100 characters. */
    public static final String PREFIX = "[gg-report]";
    /** The longest chat line the vanilla server accepts from a client. */
    public static final int MAX_LINE = 100;

    private final String viewer;
    private final String owner;
    private final String slot;
    private final String payload;

    private AutotestReport(String viewer, String owner, String slot, String payload) {
        this.viewer = viewer;
        this.owner = owner;
        this.slot = slot;
        this.payload = payload;
    }

    /**
     * Builds a report line.
     *
     * @param viewer  the client's own player name
     * @param owner   the player whose equipment is described (the viewer itself, or another player)
     * @param slot    the slot type name
     * @param payload what the client sees there, without spaces
     * @return the chat text
     * @throws IllegalArgumentException when a field contains a space or the line exceeds {@link #MAX_LINE}
     */
    public static String format(String viewer, String owner, String slot, String payload) {
        for (String field : new String[] { viewer, owner, slot, payload }) {
            if (field.isEmpty() || field.indexOf(' ') >= 0) {
                throw new IllegalArgumentException(
                    "report fields must be non-empty and have no spaces: '" + field + "'");
            }
        }
        String line = PREFIX + " " + viewer + " " + owner + " " + slot + " " + payload;
        if (line.length() > MAX_LINE) {
            throw new IllegalArgumentException(
                "report line is " + line.length() + " characters, the limit is " + MAX_LINE);
        }
        return line;
    }

    /**
     * @param text a chat line
     * @return the report, or {@code null} when the line is not a well-formed report
     */
    public static AutotestReport parse(String text) {
        if (text == null) {
            return null;
        }
        String plain = text.replaceAll("§.", "")
            .trim();
        if (!plain.startsWith(PREFIX)) {
            return null;
        }
        String[] parts = plain.substring(PREFIX.length())
            .trim()
            .split("\\s+");
        if (parts.length != 4) {
            return null;
        }
        return new AutotestReport(parts[0], parts[1], parts[2], parts[3]);
    }

    /** @return the reporting client's player name */
    public String viewer() {
        return viewer;
    }

    /** @return the player the report describes */
    public String owner() {
        return owner;
    }

    /** @return the slot type name */
    public String slot() {
        return slot;
    }

    /** @return what the client saw */
    public String payload() {
        return payload;
    }

    /**
     * @param viewer a viewer name
     * @param owner  an owner name
     * @param slot   a slot type name
     * @return the map key a collector files a report under
     */
    public static String key(String viewer, String owner, String slot) {
        return viewer + ">" + owner + ":" + slot;
    }

    /** @return {@link #key(String, String, String)} of this report */
    public String key() {
        return key(viewer, owner, slot);
    }
}
