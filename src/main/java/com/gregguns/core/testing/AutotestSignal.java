package com.gregguns.core.testing;

import java.util.Arrays;

/**
 * The text protocol between the dev-only server scenarios ({@code mp.sync}, {@code mp.visual}) and the dev-only client
 * autotest hooks. The server tells a client what to do by sending it a chat line that starts with {@link #PREFIX};
 * the client hides the line and runs the command. Pure text handling, no Minecraft types. Both sides, dev only.
 *
 * <p>
 * A line is {@code PREFIX + " " + command + " " + argument...}, for example {@code [gregguns-autotest] shot b-third}.
 */
public final class AutotestSignal {

    /** Chat-line prefix that marks a signal. */
    public static final String PREFIX = "[gregguns-autotest]";

    private AutotestSignal() {}

    /**
     * Builds a signal line.
     *
     * @param command the command word, for example {@code shot}
     * @param args    its arguments
     * @return the chat text
     */
    public static String format(String command, String... args) {
        StringBuilder out = new StringBuilder(PREFIX).append(' ')
            .append(command);
        for (String arg : args) {
            out.append(' ')
                .append(arg);
        }
        return out.toString();
    }

    /**
     * Parses a chat line.
     *
     * @param text a chat line, possibly with formatting codes in front
     * @return the command word followed by its arguments, or {@code null} if the line is not a signal
     */
    public static String[] parse(String text) {
        if (text == null) {
            return null;
        }
        String plain = text.replaceAll("\u00a7.", "")
            .trim();
        if (!plain.startsWith(PREFIX)) {
            return null;
        }
        String rest = plain.substring(PREFIX.length())
            .trim();
        if (rest.isEmpty()) {
            return null;
        }
        return rest.split("\s+");
    }

    /**
     * @param parts a parsed signal
     * @return its arguments, without the command word
     */
    public static String[] args(String[] parts) {
        return Arrays.copyOfRange(parts, 1, parts.length);
    }
}
