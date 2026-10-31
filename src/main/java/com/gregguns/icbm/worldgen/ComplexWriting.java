package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The fiction a silo complex's crew left behind (D324): notices under the room signs, clipboard lists, typed notes and
 * manuals. Every text is a lang key ({@link ComplexText}); a family is numbered from 1 and its size is found by looking
 * up keys until one is missing, so a translator or a later writer adds texts without touching code. What a room shows
 * follows its {@link RoomState}: duty rosters and the room's own work while it is staffed or in service, closing
 * notices once it is mothballed, last logs once it is gutted. Picks are by a hash the caller makes from the plan seed
 * and the piece's cell. Pure apart from the lang lookup.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md (Furniture stock: writing); D324
public final class ComplexWriting {

    /** Lang key prefixes. */
    static final String NOTICE = "icbm.complex.notice.", CLIP = "icbm.complex.clip.", BOOK = "icbm.complex.book.";

    /** Character limits: a sign line, a clipboard title, a clipboard task, a book page (the games' own limits). */
    public static final int SIGN_LINE = 15, CLIP_TITLE = 26, CLIP_TASK = 23, CLIP_TASKS = 9, BOOK_PAGE = 256;

    private static final Map<String, Integer> SIZES = new ConcurrentHashMap<>();

    private ComplexWriting() {}

    /** A clipboard's page: a title and up to nine tasks. */
    public static final class Clip {

        public final String title;
        public final List<String> tasks;

        Clip(String title, List<String> tasks) {
            this.title = title;
            this.tasks = tasks;
        }
    }

    /** A written book: title, author and pages. */
    public static final class Book {

        public final String title;
        public final String author;
        public final List<String> pages;

        Book(String title, String author, List<String> pages) {
            this.title = title;
            this.author = author;
            this.pages = pages;
        }
    }

    /** @return the lower-case name of a theme group, as the lang keys spell it */
    static String group(RoomLoot.Group g) {
        return g.name()
            .toLowerCase(Locale.ROOT);
    }

    /** @return the lower-case name of a state, as the lang keys spell it */
    static String state(RoomState s) {
        return s.name()
            .toLowerCase(Locale.ROOT);
    }

    /** @return how many keys {@code prefix + 1}, {@code prefix + 2} and so on the lang holds (cached) */
    static int size(String prefix) {
        Integer n = SIZES.get(prefix);
        if (n != null) return n;
        int i = 0;
        while (i < 64 && known(prefix + (i + 1))) i++;
        SIZES.put(prefix, i);
        return i;
    }

    private static boolean known(String key) {
        return !key.equals(ComplexText.get(key));
    }

    private static String pick(String prefix, long h) {
        int n = size(prefix);
        return n == 0 ? null : prefix + (1 + Long.remainderUnsigned(h, n));
    }

    /**
     * The notice under a room's name sign: a duty roster or the room's own work while it is worked, a closing notice
     * when mothballed, a last log when gutted.
     *
     * @param h a hash of the room
     * @return the lang key of the notice (four lines divided by {@code |})
     */
    public static String noticeKey(RoomState state, RoomLoot.Group group, long h) {
        switch (state) {
            case MOTHBALLED:
                return pick(NOTICE + "mothballed.", h);
            case GUTTED:
                return pick(NOTICE + "gutted.", h);
            default:
                boolean roster = (h >>> 40 & 1) == 0;
                String k = roster ? null : pick(NOTICE + group(group) + ".", h);
                return k != null ? k : pick(NOTICE + "roster.", h);
        }
    }

    /** @return the notice's four lines; see {@link #noticeKey} */
    public static String[] notice(RoomState state, RoomLoot.Group group, long h) {
        String k = noticeKey(state, group, h);
        return k == null ? new String[] { "", "", "", "" } : ComplexText.lines(k);
    }

    /** @return the lang key of a clipboard's list in a room of this group and state */
    public static String clipKey(RoomState state, RoomLoot.Group group, long h) {
        switch (state) {
            case MOTHBALLED:
                return pick(CLIP + "mothballed.", h);
            case GUTTED:
                return pick(CLIP + "gutted.", h);
            default:
                return pick(CLIP + group(group) + ".", h);
        }
    }

    /** @return a clipboard's list, or null when the lang holds none */
    public static Clip clip(RoomState state, RoomLoot.Group group, long h) {
        String k = clipKey(state, group, h);
        if (k == null) return null;
        String[] parts = ComplexText.get(k)
            .split("\\|", -1);
        List<String> tasks = new ArrayList<>();
        for (int i = 1; i < parts.length && tasks.size() < CLIP_TASKS; i++) tasks.add(parts[i]);
        return new Clip(parts[0], tasks);
    }

    /** @return the lang key of a room group's manual, or the site orientation for a group with none */
    public static String manualKey(RoomLoot.Group group) {
        String k = BOOK + "manual." + group(group);
        return known(k) ? k : BOOK + "manual.general";
    }

    /** @return the lang key of a crew note of this state */
    public static String noteKey(RoomState state, long h) {
        return pick(BOOK + "note." + state(state) + ".", h);
    }

    /**
     * A book for a room: its group's manual or the site orientation while worked, more often a crew note, and only
     * notes once the room is shut.
     *
     * @param h a hash of the book's place
     * @return the lang key of the book
     */
    public static String bookKey(RoomState state, RoomLoot.Group group, long h) {
        int r = (int) Long.remainderUnsigned(h >>> 20, 10);
        boolean worked = state == RoomState.STAFFED || state == RoomState.IN_SERVICE;
        if (worked && r < 4) return r < 3 ? manualKey(group) : BOOK + "manual.general";
        String note = noteKey(state, h);
        return note != null ? note : manualKey(group);
    }

    /** @return the book a key holds: title, author and pages divided by {@code ~} */
    public static Book book(String key) {
        String[] parts = ComplexText.get(key)
            .split("\\|", 3);
        String title = parts[0];
        String author = parts.length > 1 ? parts[1] : "";
        List<String> pages = parts.length > 2 ? Arrays.asList(parts[2].split("~")) : new ArrayList<String>();
        return new Book(title, author, pages);
    }
}
